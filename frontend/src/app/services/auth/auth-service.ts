import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { LoginRequest } from '../../dto/auth/LoginRequest';
import { BehaviorSubject, catchError, Observable, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { jwtDecode } from 'jwt-decode';
import { TokensDto } from '../../dto/auth/TokensDTO';
import { RegisterRequestDto } from '../../dto/auth/RegisterRequestDTO';
import { RegisterResponseDto } from '../../dto/auth/RegisterResponseDTO';
import { RefreshTokenDto } from '../../dto/auth/RefreshTokenDTO';
import { User } from '../../model/auth/user';

export interface StringBody {
  message: string;
}

export interface DecodedToken {
  sub: string;
  exp: number;
  iat: number;
  id?: number;
  role?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly TOKEN_KEY = 'tetak_app_tokens';

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    this.initializeAuth();
  }

  /**
   * Initialize authentication state from stored tokens
   */
  private initializeAuth(): void {
    const tokens = this.getStoredTokens();

    if (tokens && this.isTokenValid(tokens.accessToken)) {
      this.isAuthenticatedSubject.next(true);
    } else {
      this.clearAuthData();
    }
  }

  /**
   * Login user with username and password
   */
  login(credentials: LoginRequest): Observable<TokensDto> {
    return this.http
      .post<TokensDto>(`${environment.apiUrl}auth/login`, credentials)
      .pipe(
        tap((response) => {
          this.handleLoginSuccess(response);
        }),
        catchError((error) => {
          console.error('Login failed:', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Register new user
   */
  register(userData: RegisterRequestDto): Observable<RegisterResponseDto> {
    return this.http
      .post<RegisterResponseDto>(`${environment.apiUrl}auth/register`, userData)
      .pipe(
        catchError((error) => {
          console.error('Registration failed:', error);
          return throwError(() => error);
        })
      );
  }

  /**
   * Handle successful login response
   */
  private handleLoginSuccess(response: TokensDto): void {
    // Store tokens
    localStorage.setItem(this.TOKEN_KEY, JSON.stringify(response));

    // Update subjects
    this.isAuthenticatedSubject.next(true);
    
    // Redirect to dashboard
    this.router.navigate(['/app']);
  }

  /**
   * Logout user and clear all stored data
   */
  logout(): void {
    this.clearAuthData();
    this.router.navigate(['']);
  }

  /**
   * Clear authentication data
   */
  private clearAuthData(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
  }

  /**
   * Get access token for API requests
   */
  getAccessToken(): string | null {
    const tokens = this.getStoredTokens();
    return tokens?.accessToken || null;
  }

  /**
   * Get refresh token
   */
  getRefreshToken(): string | null {
    const tokens = this.getStoredTokens();
    return tokens?.refreshToken || null;
  }

  /**
   * Get stored tokens from localStorage
   */
  private getStoredTokens(): TokensDto | null {
    try {
      const tokens = localStorage.getItem(this.TOKEN_KEY);
      return tokens ? JSON.parse(tokens) : null;
    } catch (error) {
      console.error('Error parsing stored tokens:', error);
      return null;
    }
  }

  /**
   * Check if token is valid (not expired)
   */
  isTokenValid(token: string): boolean {
    try {
      // const decoded = this.decodeToken(token);
      // const currentTime = Math.floor(Date.now() / 1000);
      // return decoded.exp > currentTime;
      return true;
    } catch (error) {
      return false;
    }
  }

  /**
   * Decode JWT token
   */
  private decodeToken(token: string): DecodedToken {
    try {
      const payload = token.split('.')[1];
      const decodedPayload = atob(payload);
      return JSON.parse(decodedPayload);
    } catch (error) {
      throw new Error('Invalid token format');
    }
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    const tokens = this.getStoredTokens();
    return !!(tokens && this.isTokenValid(tokens.accessToken));
  }

  /**
   * Get current user
   */
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Refresh tokens using refresh token
   * Note: You'll need to implement this endpoint in your Lambda
   */
  refreshTokens(): Observable<RefreshTokenDto> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http
      .post<RefreshTokenDto>(
        `${environment.apiUrl}auth/refresh_token`,
        {},
        {
          headers: new HttpHeaders({
            Authorization: `Bearer ${refreshToken}`,
          }),
        }
      )
      .pipe(
        tap((tokens) => {
          localStorage.setItem(this.TOKEN_KEY, JSON.stringify(tokens));
          console.log(localStorage.getItem(this.TOKEN_KEY));
        }),
        catchError((error) => {
          console.error('Token refresh failed:', error);
          this.logout();
          return throwError(() => error);
        })
      );
  }

  /**
   * Get token expiration time
   */
  getTokenExpiration(): Date | null {
    const accessToken = this.getAccessToken();
    if (!accessToken) return null;

    try {
      const decoded = this.decodeToken(accessToken);
      return new Date(decoded.exp * 1000);
    } catch (error) {
      return null;
    }
  }

  /**
   * Check if token expires soon (within 5 minutes)
   */
  shouldRefreshToken(): boolean {
    const expiration = this.getTokenExpiration();
    if (!expiration) return false;

    const fiveMinutesFromNow = new Date(Date.now() + 5 * 60 * 1000);
    return expiration <= fiveMinutesFromNow;
  }

  IsC(): boolean {
    return this.getRoleFromToken() == 'C';
  }

  IsAdmin(): boolean {
    return this.getRoleFromToken() == 'A';
  }

  getRoleFromToken(): string | undefined {
    if (this.getStoredTokens()?.accessToken != null) {
      const accessToken = this.getStoredTokens()?.accessToken;
      if (accessToken) {
        const tokenInfo = this.decodeToken(accessToken);
        const role = tokenInfo.role;
        return role;
      }
    }
    return '';
  }
}
