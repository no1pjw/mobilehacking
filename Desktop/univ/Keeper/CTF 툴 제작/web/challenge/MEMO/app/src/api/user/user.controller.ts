import { Controller, Get, HttpException, Req } from '@nestjs/common';
import { UserService } from './user.service';
import { Memo, User } from 'src/common/schemas';
import { ResponseDto } from 'src/common/types';
import { Request } from 'express';
import { MemoService } from '../memo/memo.service';

@Controller('user')
export class UserController {
    constructor(
        private readonly userService: UserService,
        private readonly memoService: MemoService,
    ) { }

    @Get('/')
    async getUserInfo(@Req() req: Request): Promise<ResponseDto<{ user: User, memos: Memo[] }>> {
        const user = await this.userService.getUserByUserId(req.user._id);
        if (!user)
            throw new HttpException('User not found.', 404);

        delete user['password'];

        const memos = await this.memoService.getAllMemoByAuthor(req.user._id);

        return { status: 200, message: 'User info retrieved successfully', data: { user, memos } };
    }
}