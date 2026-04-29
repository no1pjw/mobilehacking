import { Injectable, CanActivate, ExecutionContext, HttpException } from '@nestjs/common';

@Injectable()
export class AdminGuard implements CanActivate {
    canActivate(
        context: ExecutionContext,
    ): boolean {
        const req = context.switchToHttp().getRequest();
        
        if (req.user.role !== 'admin') throw new HttpException('Unauthorized.', 401);

        return true;
    }
}