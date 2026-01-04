import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { Router, NavigationStart } from '@angular/router';
import { filter, map } from 'rxjs/operators';

interface FormattedAddress {
  street: string;
  houseNumber: string;
  city: string;
}

@Injectable({
  providedIn: 'root',
})
export class MapService {
  constructor(private http: HttpClient) {}

  search(street: string): Observable<any> {
    return this.http.get(
      `https://nominatim.openstreetmap.org/search?format=json&q=${street}`
    );
  }

  reverseSearch(lat: number, lon: number): Observable<any> {
    return this.http.get<any>(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&zoom=18&addressdetails=1`
    ).pipe(
      map(response => {
        const address = response.address;

        // Get house number or default to 'n/a'
        const number = address.house_number || '';

        // Get street name or default to 'n/a'
        const street = address.road ||address.street||address.neighbourhood||address.quarter||address.suburb
        || '';

        // Try different fields for city, as it can appear under different keys
        const city = address.city ||
                     address.town ||
                     address.village ||
                     address.municipality ||
                     address.county ||
                     '';

        return {
          street,
          number,
          city
        };
      })
    );
  }
}
