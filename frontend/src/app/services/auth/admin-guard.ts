import { CanActivate, Router } from "@angular/router";
import { AuthService } from "./auth-service";
import { Injectable } from "@angular/core";

@Injectable({
    providedIn: 'root'
  })
export class AdminGuard implements CanActivate {
  
    constructor(private authService: AuthService, private router: Router) { }
  
    canActivate(): boolean {
      if (this.authService.IsAdmin()) {
        return true;
      } else {
        return false;
      }
    }
  
}