import { HttpException, Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Memo, MemoDocument } from 'src/common/schemas';
import { CreateMemoDto } from 'src/common/dto';
import { randomUUID } from 'crypto';

@Injectable()
export class MemoService {
    constructor(
        @InjectModel(Memo.name)
        private memoModel: Model<MemoDocument>,
    ) { }

    private isUuidV4(value: string): boolean {
        const uuidV4Regex =
            /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
        return uuidV4Regex.test(value);
    }

    async getAllMemoByAuthor(author: string): Promise<Memo[]> {
        const memos = await this.memoModel.find({ author: new Types.ObjectId(author) }).lean();

        return memos;
    }

    async getMemoById(id: string, author: string): Promise<Memo> {
        if (!Types.ObjectId.isValid(id))
            throw new HttpException('Invalid memo id', 400);

        const memo = await this.memoModel.findOne({
            _id: new Types.ObjectId(id),
            author: new Types.ObjectId(author),
        }).lean();

        if (!memo)
            throw new HttpException('Memo not found', 404);

        return memo;
    }

    async createMemo(data: CreateMemoDto, author: string): Promise<void> {
        const replacedContent = data.content.replace(/@\(([a-zA-Z0-9.]+)\)/g, (match, filename) => {
            return `<img src="/api/image?filename=${filename}">`;
        });

        await this.memoModel.create({
            title: data.title,
            content: replacedContent,
            author: new Types.ObjectId(author),
        });

        return;
    }

    async incrementViews(id: string): Promise<void> {
        if (!Types.ObjectId.isValid(id))
            throw new HttpException('Invalid memo id', 400);

        const memo = await this.memoModel.findById(new Types.ObjectId(id)).lean();

        if (!memo)
            throw new HttpException('Memo not found', 404);

        await this.memoModel.findByIdAndUpdate(new Types.ObjectId(id), { $inc: { views: 1 } });

        return;
    }

    async createShareKey(id: string, author: string): Promise<string> {
        if (!author)
            throw new HttpException('Unauthorized', 401);

        if (!Types.ObjectId.isValid(id))
            throw new HttpException('Invalid memo id', 400);

        const memo = await this.memoModel.findOne({
            _id: new Types.ObjectId(id),
            author: new Types.ObjectId(author),
        });

        if (!memo)
            throw new HttpException('Memo not found', 404);

        const sharedKey = randomUUID();
        memo.sharedKey = sharedKey;
        await memo.save();

        return sharedKey;
    }

    async getMemoByShareKey(sharedKey: string): Promise<Memo> {
        if (!this.isUuidV4(sharedKey))
            throw new HttpException('Invalid share key', 400);

        const memo = await this.memoModel.findOne({ sharedKey }).lean();

        if (!memo)
            throw new HttpException('Memo not found', 404);

        return memo;
    }
}