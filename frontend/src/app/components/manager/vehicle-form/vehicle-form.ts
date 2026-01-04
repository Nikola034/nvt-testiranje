import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { VehicleService } from '../../../services/vehicle/vehicle.service';
import { VehicleBrandDTO } from '../../../dto/vehicle/VehicleBrandDTO';
import { VehicleModelDTO } from '../../../dto/vehicle/VehicleModelDTO';
import { VehicleResponseDTO } from '../../../dto/vehicle/VehicleResponseDTO';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { FileUploadModule } from 'primeng/fileupload';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { DividerModule } from 'primeng/divider';
import { MessageService } from 'primeng/api';

interface ImageWithUrl {
  id: number;
  originalName: string;
  url: string;
}

@Component({
  selector: 'app-vehicle-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    FileUploadModule,
    ProgressSpinnerModule,
    MessageModule,
    ToastModule,
    DividerModule
  ],
  providers: [MessageService],
  templateUrl: './vehicle-form.html',
  styleUrl: './vehicle-form.scss'
})
export class VehicleFormComponent implements OnInit {
  vehicleForm!: FormGroup;
  isEditMode = false;
  vehicleId: number | null = null;
  loading = false;
  submitting = false;

  brands: VehicleBrandDTO[] = [];
  models: VehicleModelDTO[] = [];
  selectedBrand: VehicleBrandDTO | null = null;

  existingImages: ImageWithUrl[] = [];
  imagesToDelete: number[] = [];
  newImages: File[] = [];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private vehicleService: VehicleService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.initForm();
    
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      this.isEditMode = true;
      this.vehicleId = +id;
    }

    this.loadBrands();
  }

  initForm(): void {
    this.vehicleForm = this.fb.group({
      licensePlate: ['', [Validators.required, Validators.minLength(2)]],
      weightLimit: [null, [Validators.required, Validators.min(1)]],
      brand: [null, Validators.required],
      model: [null, Validators.required]
    });
  }

  loadBrands(): void {
    this.loading = true;
    this.vehicleService.getAllBrands().subscribe({
      next: (brands) => {
        this.brands = brands;
        if (this.isEditMode && this.vehicleId) {
          this.loadVehicle(this.vehicleId);
        } else {
          this.loading = false;
        }
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load brands' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  onBrandChange(event: any): void {
    this.selectedBrand = event.value;
    this.vehicleForm.patchValue({ model: null });
    this.models = [];

    if (this.selectedBrand) {
      this.vehicleService.getModelsByBrand(this.selectedBrand.id).subscribe({
        next: (models) => {
          this.models = models;
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load models' });
          console.error(err);
        }
      });
    }
  }

  loadVehicle(id: number): void {
    this.vehicleService.getById(id).subscribe({
      next: (vehicle) => {
        const brand = this.brands.find(b => b.id === vehicle.brandId);
        if (brand) {
          this.selectedBrand = brand;
          this.vehicleService.getModelsByBrand(brand.id).subscribe({
            next: (models) => {
              this.models = models;
              const model = models.find(m => m.id === vehicle.modelId);
              this.vehicleForm.patchValue({
                licensePlate: vehicle.licensePlate,
                weightLimit: vehicle.weightLimit,
                brand: brand,
                model: model
              });
              this.loading = false;
            },
            error: () => {
              this.loading = false;
            }
          });
        } else {
          this.vehicleForm.patchValue({
            licensePlate: vehicle.licensePlate,
            weightLimit: vehicle.weightLimit
          });
          this.loading = false;
        }
        this.loadExistingImages(id);
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load vehicle' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadExistingImages(vehicleId: number): void {
    this.vehicleService.getVehicleImages(vehicleId).subscribe({
      next: (data) => {
        this.existingImages = data.images;
      },
      error: (err) => {
        console.error('Failed to load images', err);
      }
    });
  }

  onImageSelect(event: any): void {
    for (const file of event.files) {
      if (!this.newImages.find(f => f.name === file.name)) {
        this.newImages.push(file);
      }
    }
  }

  removeNewImage(index: number): void {
    this.newImages.splice(index, 1);
  }

  markImageForDeletion(imageId: number): void {
    if (!this.imagesToDelete.includes(imageId)) {
      this.imagesToDelete.push(imageId);
    }
  }

  restoreImage(imageId: number): void {
    const index = this.imagesToDelete.indexOf(imageId);
    if (index > -1) {
      this.imagesToDelete.splice(index, 1);
    }
  }

  isMarkedForDeletion(imageId: number): boolean {
    return this.imagesToDelete.includes(imageId);
  }

  onSubmit(): void {
    if (this.vehicleForm.invalid) {
      this.vehicleForm.markAllAsTouched();
      return;
    }

    const remainingImages = this.existingImages.filter(img => !this.imagesToDelete.includes(img.id)).length;
    if (!this.isEditMode && this.newImages.length === 0) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'At least one image is required' });
      return;
    }
    if (this.isEditMode && remainingImages + this.newImages.length === 0) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'At least one image is required' });
      return;
    }

    this.submitting = true;
    const formValue = this.vehicleForm.value;

    if (this.isEditMode && this.vehicleId) {
      const updateData = {
        licensePlate: formValue.licensePlate,
        weightLimit: formValue.weightLimit,
        brandId: formValue.brand.id,
        modelId: formValue.model.id,
        imagesToDelete: this.imagesToDelete
      };

      this.vehicleService.update(this.vehicleId, updateData, this.newImages.length > 0 ? this.newImages : undefined).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Vehicle updated successfully' });
          setTimeout(() => this.router.navigate(['/app/manager/vehicles']), 1000);
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error || 'Failed to update vehicle' });
          this.submitting = false;
        }
      });
    } else {
      const createData = {
        licensePlate: formValue.licensePlate,
        weightLimit: formValue.weightLimit,
        brandId: formValue.brand.id,
        modelId: formValue.model.id
      };

      this.vehicleService.create(createData, this.newImages).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Vehicle created successfully' });
          setTimeout(() => this.router.navigate(['/app/manager/vehicles']), 1000);
        },
        error: (err) => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error || 'Failed to create vehicle' });
          this.submitting = false;
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/app/manager/vehicles']);
  }
}
