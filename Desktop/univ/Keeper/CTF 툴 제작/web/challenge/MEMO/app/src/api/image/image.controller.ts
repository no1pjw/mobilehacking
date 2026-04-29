import { HttpException, Controller, Get, Query, Req, Res, UseGuards } from '@nestjs/common';
import { ImageService } from './image.service';
import { Request, Response } from 'express';
import { AdminGuard } from 'src/common/guard';

@Controller('image')
export class ImageController {
    constructor(private readonly imageService: ImageService) { }

    @Get('/')
    async getImage(
        @Query('filename') filename: string,
        @Res() res: Response
    ): Promise<void> {
        if (!filename) throw new HttpException('filename is required.', 400);

        const imagePath = this.imageService.getImagePath(filename);
        if (!imagePath) return;

        return res.sendFile(imagePath);
    }

    @Get('/admin')
    @UseGuards(AdminGuard)
    async getImageAdmin(
        @Query('filename') filename: string,
        @Req() req: Request,
        @Res() res: Response
    ): Promise<void> {
        const site = req.get('sec-fetch-site');

        if (site !== 'same-origin') throw new HttpException('Unauthorized.', 401);

        if (!filename) throw new HttpException('filename is required.', 400);

        const imagePath = this.imageService.getAdminImagePath(filename);
        if (!imagePath) return;

        return res.sendFile(imagePath);
    }
}