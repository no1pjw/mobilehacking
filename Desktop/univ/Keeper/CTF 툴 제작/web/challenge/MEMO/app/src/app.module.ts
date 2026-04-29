import {
  MiddlewareConsumer,
  Module,
  NestModule,
  RequestMethod,
} from '@nestjs/common';
import { AppService } from './app.service';
import { JwtExpireMiddleware, LoggerMiddleware } from './common/middlewares';
import { JwtModule } from '@nestjs/jwt';
import { ConfigModule } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from './api/auth/auth.module';
import { UserModule } from './api/user/user.module';
import { ImageModule } from './api/image/image.module';
import { MemoModule } from './api/memo/memo.module';

ConfigModule.forRoot();

@Module({
  imports: [
    MongooseModule.forRoot(process.env.MONGO_URI),
    JwtModule.register({
      global: true,
      secret: process.env.JWT_SECRET_KEY,
      signOptions: { expiresIn: '30m' },
    }),
    AuthModule,
    UserModule,
    ImageModule,
    MemoModule,
  ],
  controllers: [],
  providers: [AppService],
})

export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer) {
    consumer.apply(LoggerMiddleware).forRoutes('{*path}');
    consumer
      .apply(JwtExpireMiddleware)
      .exclude(
        { path: 'auth', method: RequestMethod.ALL },
        { path: 'auth/*splat', method: RequestMethod.ALL },

        { path: 'image', method: RequestMethod.ALL },

        { path: 'memo/:id/view', method: RequestMethod.POST },
        { path: 'memo/shared/:uuid', method: RequestMethod.GET },
      )
      .forRoutes({ path: '*splat', method: RequestMethod.ALL })
  }
}
