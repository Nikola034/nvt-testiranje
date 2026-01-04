import { Component, OnInit, PLATFORM_ID, Inject, Input, ViewEncapsulation, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MapService } from './map.service';
import { LeafletHelper } from './leaflet.helper';
// PrimeNG Imports
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { PanelModule } from 'primeng/panel';
import { ScrollPanelModule } from 'primeng/scrollpanel';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { FormsModule } from '@angular/forms';
import { combineLatest } from 'rxjs';
import { AddressDTO } from '../../dto/auth/AddressDTO';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    InputTextModule,
    ButtonModule,
    CardModule,
    PanelModule,
    ScrollPanelModule,
    AutoCompleteModule
  ],
  templateUrl: './map.component.html',
  styleUrl: './map.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class MapComponent implements OnInit {
  @Input() showSearch: boolean = false;

  private map: any;
  private L: any;

  searchQuery: string = '';
  searchResults: any[] = [];
  searchMarkers: any[] = [];
  currentMarkers: any = { 'event': [], 'product': [], 'service': [] };
  private markerIcons: { [key: string]: any } = {};
  lat = 45.25710603831593;
  lng = 19.84540080257916;
  defaultAddress:boolean=true;
  @Output() addressSelected = new EventEmitter<AddressDTO>();

  constructor(
    private leafletHelper: LeafletHelper,
    @Inject(PLATFORM_ID) private platformId: Object,
    private mapService: MapService
  ) { }

  ngOnInit() {
    if (this.platformId === 'browser') {
      this.leafletHelper.loadLeaflet().then((L) => {
        this.L = L;
        this.initializeMarkerIcons();
        this.initMap();

      });
    }
  }



  markAllAddresses(addresses: AddressDTO[], type: string = 'default') {
    this.currentMarkers[type].forEach((marker: any) => this.map.removeLayer(marker));
    this.currentMarkers[type] = [];
    addresses.forEach((address) => {
      if(this.defaultAddress){
        this.lat=address.latitude??0;
        this.lng=address.longitude??0;
        this.defaultAddress=false;
        this.map.setView([this.lat, this.lng], this.map.getZoom());
      }
      this.currentMarkers[type].push(this.addMarker(address.latitude, address.longitude, type));
    })
  }

  private createColoredIcon(color: string) {
    return this.L.divIcon({
      html: `
        <svg width="24" height="36" viewBox="0 0 24 36" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 0C5.37 0 0 5.37 0 12c0 9 12 24 12 24s12-15 12-24c0-6.63-5.37-12-12-12z" fill="${color}"/>
          <circle cx="12" cy="12" r="4" fill="white"/>
        </svg>
      `,
      className: 'custom-marker',
      iconSize: [24, 36],
      iconAnchor: [12, 36],
      popupAnchor: [0, -36]
    });
  }

  private initializeMarkerIcons() {
    this.markerIcons = {
      event: this.createColoredIcon('#ff8800'),
      service: this.createColoredIcon('#ff4444'),
      product: this.createColoredIcon('#0fa8ab'),
      default: this.createColoredIcon('#00cc44')
    };
  }

  public initMap(): void {
    this.map = this.L.map('map', {
      layers: [
        this.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 21,
          maxNativeZoom: 19,
          minZoom: 3,
          attribution: '...',
          noWrap: true,
        }),
      ],
      zoom: 13,
      center: this.L.latLng(this.lat, this.lng),
    });
    if (this.showSearch)
      this.registerOnClick(this.L);
    this.map.invalidateSize();
  }




  private addMarker(lat: number | undefined | null, lng: number | undefined | null, type: string = 'default', popupContent?: string) {
    const marker = this.L.marker([lat, lng], {
      icon: this.markerIcons[type] || this.markerIcons['default']
    });

    if (popupContent) {
      marker.bindPopup(popupContent);
    }

    marker.addTo(this.map);
    return marker;
  }

  private registerOnClick(L: any): void {
    this.map.on('click', (e: any) => {
      this.searchMarkers.forEach(marker => this.map.removeLayer(marker));
      this.searchMarkers = [];
      const { lat, lng } = e.latlng;
      this.performReverseSearch(lat, lng);
      const marker = this.addMarker(lat, lng);
      this.searchMarkers.push(marker);
      this.map.setView([lat, lng], 15);
    });
  }

  onSearch(event: any): void {
    const query = event.query;
    if (!query.trim()) return;

    this.mapService.search(query).subscribe({
      next: (results) => {
        this.searchResults = results;
      },
      error: (err) => {
        console.error('Search error', err);
      }
    });
  }

  private performReverseSearch(lat: number, lng: number): void {
    this.mapService.reverseSearch(lat, lng).subscribe({
      next: (res) => {
        const address: AddressDTO = {
          street: res.street,
          city: res.city,
          number: res.number,
          latitude: Number(lat),
          longitude: Number(lng)
        };
        this.addressSelected.emit(address);
      },
      error: (err) => {
        console.error('Reverse search error', err);
      }
    });
  }


  selectSearchResult(result: any): void {
    this.searchMarkers.forEach(marker => this.map.removeLayer(marker));
    this.searchMarkers = [];

    this.performReverseSearch(result.value.lat,result.value.lon);
    const marker = this.addMarker(result.value.lat, result.value.lon, 'default', result.value.display_name);
    this.searchMarkers.push(marker);
    this.map.setView([result.value.lat, result.value.lon], 15);
  }
}
