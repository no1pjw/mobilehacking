import { Global, Module } from '@nestjs/common';
import { ImageController } from './image.controller';
import { ImageService } from './image.service';

@Global()
@Module({
    imports: [],
    controllers: [ImageController],
    providers: [ImageService],
    exports: [ImageService],
})
export class ImageModule { }