import { IsIn, IsString } from 'class-validator';

export const deployment = ['prod', 'dev'] as const;
export type Deployment = (typeof deployment)[number];

export class ClusterDto {
  @IsString()
  name: string;

  @IsString()
  version: string;

  @IsString()
  description: string;

  @IsIn(deployment)
  mode: Deployment;

  @IsString()
  hostname: string;

  @IsString()
  author: string;
}
