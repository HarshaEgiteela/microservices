import { Injectable } from '@angular/core';
import {
  HttpClient
} from '@angular/common/http';

import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { User } from '../models/app.models';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(
    private readonly http: HttpClient
  ) {}

  getAll(): Observable<User[]> {

    return this.http.get<User[]>(
      `${API_BASE_URL}/users`
    );
  }
}
