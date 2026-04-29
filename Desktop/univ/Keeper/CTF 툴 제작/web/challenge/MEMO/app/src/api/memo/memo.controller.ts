import { Controller, Body, Get, Req, Post, Param } from '@nestjs/common';
import { MemoService } from './memo.service';
import { Memo } from 'src/common/schemas';
import { ResponseDto } from 'src/common/types';
import { Request } from 'express';
import { CreateMemoDto } from 'src/common/dto';

@Controller('memo')
export class MemoController {
    constructor(private readonly memoService: MemoService) { }

    @Get('/')
    async getAllMemoByAuthor(@Req() req: Request): Promise<ResponseDto<Memo[]>> {
        const memos = await this.memoService.getAllMemoByAuthor(req.user._id);

        return { status: 200, message: 'Memo list retrieved successfully', data: memos };
    }

    @Post('/')
    async createMemo(
        @Body() data: CreateMemoDto,
        @Req() req: Request
    ): Promise<ResponseDto> {
        await this.memoService.createMemo(data, req.user._id);

        return { status: 200, message: 'Memo created successfully' };
    }

    @Get('/:id')
    async getMemoById(
        @Req() req: Request,
        @Param('id') id: string
    ): Promise<ResponseDto<Memo>> {
        const memo = await this.memoService.getMemoById(id, req.user._id);

        return { status: 200, message: 'Memo retrieved successfully', data: memo };
    }

    @Post('/:id/view')
    async incrementViews(@Param('id') id: string): Promise<ResponseDto> {
        await this.memoService.incrementViews(id);

        return { status: 200, message: 'Views incremented successfully' };
    }

    @Post('/:id/share')
    async shareMemo(
        @Param('id') id: string,
        @Req() req: Request,
    ): Promise<ResponseDto<{ sharedKey: string }>> {
        const sharedKey = await this.memoService.createShareKey(id, req.user._id);

        return { status: 200, message: 'Memo shared', data: { sharedKey } };
    }

    @Get('/shared/:uuid')
    async getSharedMemo(@Param('uuid') uuid: string): Promise<ResponseDto<Memo>> {
        const memo = await this.memoService.getMemoByShareKey(uuid);

        return { status: 200, message: 'Memo retrieved successfully', data: memo };
    }
}