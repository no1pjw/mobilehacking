import { IsNotEmpty, IsString } from 'class-validator';

export class CreateMemoDto {
    @IsString()
    @IsNotEmpty()
    readonly title: string;

    @IsString()
    @IsNotEmpty()
    readonly content: string;
}