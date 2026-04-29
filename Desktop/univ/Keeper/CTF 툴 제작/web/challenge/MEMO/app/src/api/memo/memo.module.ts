import { Global, Module } from '@nestjs/common';
import { MemoController } from './memo.controller';
import { MemoService } from './memo.service';
import { Memo, MemoSchema } from 'src/common/schemas';
import { MongooseModule } from '@nestjs/mongoose';

@Global()
@Module({
    imports: [MongooseModule.forFeature([{ name: Memo.name, schema: MemoSchema }])],
    controllers: [MemoController],
    providers: [MemoService],
    exports: [MemoService],
})
export class MemoModule { }