import { HttpException, Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { CreateUserDto } from 'src/common/dto';
import { User, UserDocument } from 'src/common/schemas';

import * as bcrypt from 'bcrypt';

@Injectable()
export class UserService {
    constructor(
        @InjectModel(User.name)
        private userModel: Model<UserDocument>,
    ) { }

    async getUserByUserId(userId: string): Promise<User> {
        const user = await this.userModel.findOne({ _id: new Types.ObjectId(userId) }).lean();

        return user;
    }

    async getUserByUsername(username: string): Promise<User> {
        const user = await this.userModel.findOne({ username: username }).lean();

        return user;
    }

    async createUser(data: CreateUserDto): Promise<User> {
        const hashedPassword = await bcrypt.hash(data.password, 10);

        const user = await this.userModel.create({
            username: data.username,
            password: hashedPassword,
            name: data.name,
            role: 'user',
        });

        return user.toObject();
    }
}