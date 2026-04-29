import { Injectable } from '@nestjs/common';
import { existsSync, readdirSync } from 'fs';
import { join, resolve } from 'path';

@Injectable()
export class ImageService {
    private getImageDir(): string {
        return join(__dirname, '..', '..', 'images');
    }

    private resolveSafePath(filename: string): string | null {
        const baseDir = resolve(this.getImageDir());
        const resolvedPath = resolve(baseDir, filename);

        if (!resolvedPath.startsWith(baseDir + '/')) return null;

        return resolvedPath;
    }

    getImagePath(filename: string): string | null {
        const imagePath = this.resolveSafePath(filename);

        if (!imagePath || !existsSync(imagePath)) return null;

        return imagePath;
    }

    getAdminImagePath(filename: string): string | null {
        const imagePath = this.resolveSafePath(filename);

        if (imagePath && existsSync(imagePath)) return imagePath;

        const files = readdirSync(this.getImageDir());
        const matched = files.find(file => file.startsWith(filename));

        if (!matched) return null;

        return this.resolveSafePath(matched);
    }
}