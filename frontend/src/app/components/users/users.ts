import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-users',
  imports: [],
  templateUrl: './users.html',
  styleUrl: './users.css'
})
export class Users implements OnInit {

  users: any[] = [];

  constructor(private userService: UserService) {}

  ngOnInit(): void {

    // Load users when page starts
    this.loadUsers();

    // Reload users whenever registration succeeds
    this.userService.userRegistered$.subscribe(() => {
      this.loadUsers();
    });
  }

  loadUsers(): void {

    this.userService.getUsers().subscribe({
      next: (data) => {
        this.users = data as any[];
      },

      error: (error) => {
        console.error('Failed to load users:', error);
      }
    });

  }
}
