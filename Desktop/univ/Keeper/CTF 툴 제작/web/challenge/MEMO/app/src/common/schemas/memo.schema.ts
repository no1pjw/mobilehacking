import { Prop, Schema, SchemaFactory, SchemaOptions } from '@nestjs/mongoose';
import { Document } from 'mongoose';
import { Types } from 'mongoose';

export type MemoDocument = Memo & Document;

const options: SchemaOptions = {
    timestamps: true,
    versionKey: false,
};

@Schema(options)
export class Memo {
    @Prop({
        required: true,
    })
    title: string;

    @Prop({
        required: true,
    })
    content: string;

    @Prop({
        required: true,
        type: Types.ObjectId,
        ref: 'User',
    })
    author: Types.ObjectId;

    @Prop({
        required: true,
        default: 0
    })
    views: number;

    @Prop({
        required: false,
        unique: true,
        sparse: true,
    })
    sharedKey?: string;
}

export const MemoSchema = SchemaFactory.createForClass(Memo);