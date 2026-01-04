import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  BehaviorSubject,
  catchError,
  filter,
  finalize,
  Observable,
  switchMap,
  take,
  throwError,
} from 'rxjs';
import { AuthService } from './auth-service';
import { RefreshTokenDto } from '../../dto/auth/RefreshTokenDTO';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  // URLs that should not include the Authorization header
  private readonly excludedUrls = [
    '/auth/login',
    '/auth/register',
    '/auth/refresh_token',
    'https://api.pwnedpasswords.com/range/'
  ];

  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Check if this request should be excluded from authentication
    if (this.shouldExcludeUrl(request.url)) {
      return next.handle(request);
    }

    // Add auth header if user is authenticated
    const authRequest = this.addAuthHeader(request);

    return next.handle(authRequest).pipe(
      catchError(error => {
        if (error instanceof HttpErrorResponse) {
          // Handle 401 Unauthorized errors
          if (error.status === 401) {
            return this.handle401Error(authRequest, next);
          }
          
          // Handle 403 Forbidden errors
          if (error.status === 403) {
            console.warn('Access forbidden - insufficient permissions');
            this.authService.logout();
            return throwError(() => error);
          }
        }
        
        return throwError(() => error);
      })
    );
  }

  /**
   * Add Authorization header to request
   */
  private addAuthHeader(request: HttpRequest<any>): HttpRequest<any> {
    const accessToken = this.authService.getAccessToken();
    
    // Don't set Content-Type for FormData - let the browser set it with the boundary
    const isFormData = request.body instanceof FormData;
    
    if (accessToken && this.authService.isAuthenticated()) {
      const headers: { [key: string]: string } = {
        Authorization: `Bearer ${accessToken}`
      };
      if (!isFormData) {
        headers['Content-Type'] = 'application/json';
      }
      return request.clone({ setHeaders: headers });
    }
    
    if (!isFormData) {
      return request.clone({
        setHeaders: {
          'Content-Type': 'application/json'
        }
      });
    }
    
    return request;
  }

  /**
   * Check if URL should be excluded from authentication
   */
  private shouldExcludeUrl(url: string): boolean {
    return this.excludedUrls.some(excludedUrl => url.includes(excludedUrl));
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
    this.isRefreshing = true;
    this.refreshTokenSubject.next(null);
    console.log('trying to refresh token');

    if (this.authService.getRefreshToken()) {
      return this.authService.refreshTokens().pipe(
        switchMap((tokens: RefreshTokenDto) => {
          console.log('✅ Got new tokens:', tokens);
          
          // 🔥 SAČUVAJ TOKENS TRAJNO OVDE!
          localStorage.setItem("secure_app_tokens", JSON.stringify(tokens));
          console.log('✅ Tokens permanently saved to localStorage');
          
          this.isRefreshing = false;
          this.refreshTokenSubject.next(tokens.accessToken);
          
          // Retry sa novim tokenom
          const newRequest = request.clone({
            setHeaders: {
              Authorization: `Bearer ${tokens.accessToken}`
            }
          });
          
          console.log('🔄 Retrying request with new token');
          return next.handle(newRequest);
        }),
        catchError((error) => {
          console.error('❌ Refresh failed in interceptor:', error);
          this.isRefreshing = false;
          this.refreshTokenSubject.next(null);
          this.authService.logout();
          return throwError(() => error);
        }),
        finalize(() => {
          this.isRefreshing = false;
        })
      );
    } else {
      this.isRefreshing = false;
      this.authService.logout();
      return throwError(() => new Error('Authentication required'));
    }
    } else {
      // Token refresh is already in progress, wait for it to complete
      return this.refreshTokenSubject.pipe(
        filter(token => token != null),
        take(1),
        switchMap(() => {
          const newRequest = this.addAuthHeader(request);
          return next.handle(newRequest);
        })
      );
    }
  }
}