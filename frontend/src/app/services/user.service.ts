import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private apiUrl = 'http://localhost:8080/users';

  private userRegisteredSource = new Subject<void>();

  userRegistered$ = this.userRegisteredSource.asObservable();

  constructor(private http: HttpClient) {}

  getUsers() {
    return this.http.get(this.apiUrl);
  }

  register(user: any) {
    return this.http.post(this.apiUrl, user);
  }

  notifyUserRegistered() {
    this.userRegisteredSource.next();
  }
}
