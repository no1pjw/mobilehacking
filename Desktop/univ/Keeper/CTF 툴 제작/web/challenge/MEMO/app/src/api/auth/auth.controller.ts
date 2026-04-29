import { Body, Controller, Post, Res } from '@nestjs/common';
import { CreateUserDto, LoginDto } from 'src/common/dto';
import { AuthService } from './auth.service';
import { TokenResponse, ResponseDto } from 'src/common/types';
import { Response } from 'express';

@Controller('auth')
export class AuthController {
    constructor(private readonly authService: AuthService) { }

    @Post('/login')
    async login(@Body() data: LoginDto, @Res() res: Response): Promise<any> {
        const user = await this.authService.login(data);
        const { accessToken } = await this.authService.getAccessToken(user);
        res.cookie('accessToken', accessToken, {
            httpOnly: true,
            secure: true,
            sameSite: 'none',
            maxAge: 1000 * 60 * 60 * 24 * 30,
        });

        return res.json({ status: 200, message: 'Login successful' });
    }

    @Post('/register')
    async register(@Body() data: CreateUserDto): Promise<ResponseDto> {
        await this.authService.register(data);

        return { status: 200, message: 'User registered successfully' };
    }
}