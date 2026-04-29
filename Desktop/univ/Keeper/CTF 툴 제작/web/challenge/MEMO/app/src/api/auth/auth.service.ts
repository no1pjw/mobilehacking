import { HttpException, Injectable } from '@nestjs/common';
import { LoginDto, CreateUserDto } from 'src/common/dto';
import { User } from 'src/common/schemas';

import * as bcrypt from 'bcrypt';
import { JwtService } from '@nestjs/jwt';
import { UserService } from '../user/user.service';
import { TokenResponse } from 'src/common/types';

@Injectable()
export class AuthService {
    constructor(
        private userService: UserService,

        private readonly jwtService: JwtService,
    ) { }

    private async verifyPassword(
        password: string,
        hashedPassword: string,
    ): Promise<boolean> {
        return await bcrypt.compare(password, hashedPassword);
    }

    async login(data: LoginDto): Promise<User> {
        const user = await this.userService.getUserByUsername(data.username);
        if (!user)
            throw new HttpException('Invalid username or password.', 404);

        const validPassword = await this.verifyPassword(data.password, user.password);
        if (!validPassword)
            throw new HttpException('Invalid username or password.', 404);

        return user;
    }

    async register(data: CreateUserDto): Promise<User> {
        const existingUser = await this.userService.getUserByUsername(data.username);
        if (existingUser)
            throw new HttpException('Username already exists.', 400);

        const user = await this.userService.createUser(data);

        return user;
    }

    async getAccessToken(user: User): Promise<TokenResponse> {
        delete user['password'];

        const payload = { ...user };

        const accessToken = this.jwtService.sign(payload);

        return { accessToken };
    }
}