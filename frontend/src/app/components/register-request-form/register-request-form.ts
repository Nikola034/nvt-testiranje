import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MapComponent } from '../map/map.component';
import { CompanyRegistrationService } from '../../services/company/company-registration.service';
import { CountryDTO } from '../../dto/company/CountryDTO';
import { CityDTO } from '../../dto/company/CityDTO';
import { CreateRequestDTO } from '../../dto/company/CreateRequestDTO';
import { AddressDTO } from '../../dto/auth/AddressDTO';

// PrimeNG imports
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { FileUploadModule } from 'primeng/fileupload';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { DividerModule } from 'primeng/divider';
import { MessageModule } from 'primeng/message';

@Component({
  selector: 'app-register-request-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MapComponent,
    CardModule,
    InputTextModule,
    SelectModule,
    ButtonModule,
    FileUploadModule,
    ToastModule,
    DividerModule,
    MessageModule
  ],
  providers: [MessageService],
  templateUrl: './register-request-form.html',
  styleUrl: './register-request-form.scss',
})
export class RegisterRequestForm implements OnInit {
  registrationForm!: FormGroup;
  
  countries: CountryDTO[] = [];
  cities: CityDTO[] = [];
  
  selectedCountry: CountryDTO | null = null;
  selectedCity: CityDTO | null = null;
  
  companyImages: File[] = [];
  proofDocuments: File[] = [];
  
  selectedLatitude: number | null = null;
  selectedLongitude: number | null = null;
  selectedStreet: string = '';
  
  isSubmitting = false;
  locationSelected = false;

  constructor(
    private fb: FormBuilder,
    private companyRegistrationService: CompanyRegistrationService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadCountries();
  }

  private initForm(): void {
    this.registrationForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      country: [null, Validators.required],
      city: [null, Validators.required],
      street: ['', Validators.required],
      streetNumber: ['', Validators.required]
    });
  }

  private loadCountries(): void {
    this.companyRegistrationService.getCountries().subscribe({
      next: (countries) => {
        this.countries = countries;
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Unable to load countries list'
        });
        console.error('Error loading countries', err);
      }
    });
  }

  onCountryChange(event: any): void {
    this.selectedCountry = event.value;
    this.cities = [];
    this.selectedCity = null;
    this.registrationForm.patchValue({ city: null });

    if (this.selectedCountry) {
      this.companyRegistrationService.getCitiesByCountry(this.selectedCountry.id).subscribe({
        next: (cities) => {
          this.cities = cities;
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Unable to load cities list'
          });
          console.error('Error loading cities', err);
        }
      });
    }
  }

  onCityChange(event: any): void {
    this.selectedCity = event.value;
  }

  onAddressSelected(address: AddressDTO): void {
    this.selectedLatitude = address.latitude ?? null;
    this.selectedLongitude = address.longitude ?? null;
    this.selectedStreet = address.street ?? '';
    this.locationSelected = true;
    
    // Update street and street number in the form
    this.registrationForm.patchValue({
      street: this.selectedStreet,
      streetNumber: address.number ?? ''
    });

    this.messageService.add({
      severity: 'success',
      summary: 'Location Selected',
      detail: `Address: ${this.selectedStreet || 'Location marked on map'}`
    });
  }

  onImageSelect(event: any): void {
    for (let file of event.files) {
      this.companyImages.push(file);
    }
  }

  onImageRemove(event: any): void {
    const index = this.companyImages.indexOf(event.file);
    if (index > -1) {
      this.companyImages.splice(index, 1);
    }
  }

  onImageClear(): void {
    this.companyImages = [];
  }

  onDocumentSelect(event: any): void {
    for (let file of event.files) {
      this.proofDocuments.push(file);
    }
  }

  onDocumentRemove(event: any): void {
    const index = this.proofDocuments.indexOf(event.file);
    if (index > -1) {
      this.proofDocuments.splice(index, 1);
    }
  }

  onDocumentClear(): void {
    this.proofDocuments = [];
  }

  isFormValid(): boolean {
    return (
      this.registrationForm.valid &&
      this.locationSelected &&
      this.companyImages.length > 0 &&
      this.proofDocuments.length > 0
    );
  }

  onSubmit(): void {
    if (!this.isFormValid()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Incomplete Data',
        detail: 'Please fill in all required fields, select a location on the map, and add the required files.'
      });
      return;
    }

    this.isSubmitting = true;

    const requestData: CreateRequestDTO = {
      name: this.registrationForm.get('name')?.value,
      countryId: this.selectedCountry!.id,
      cityId: this.selectedCity!.id,
      street: this.registrationForm.get('street')?.value,
      streetNumber: this.registrationForm.get('streetNumber')?.value,
      latitude: this.selectedLatitude!,
      longitude: this.selectedLongitude!
    };

    this.companyRegistrationService.createRegistrationRequest(
      requestData,
      this.companyImages,
      this.proofDocuments
    ).subscribe({
      next: (requestId) => {
        this.isSubmitting = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: `Company registration request created successfully (ID: ${requestId})`
        });
        this.resetForm();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: err.error || 'An error occurred while creating the request'
        });
        console.error('Error creating registration request', err);
      }
    });
  }

  private resetForm(): void {
    this.registrationForm.reset();
    this.selectedCountry = null;
    this.selectedCity = null;
    this.cities = [];
    this.companyImages = [];
    this.proofDocuments = [];
    this.selectedLatitude = null;
    this.selectedLongitude = null;
    this.selectedStreet = '';
    this.locationSelected = false;
  }
}
