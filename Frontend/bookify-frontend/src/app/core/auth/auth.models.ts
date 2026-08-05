export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface AuthMessageResponse {
  message: string;
}

export interface ApiError {
  status: number;
  code: string;
  message: string;
  correlationId?: string;
  validationErrors?: Record<string, string>;
}
