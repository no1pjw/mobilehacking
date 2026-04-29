import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

import helmet from 'helmet';
import { ValidationPipe } from '@nestjs/common';
import { NotFoundFilter, UnauthorizedFilter } from './common/filters';
import { join } from 'path';
import { NestExpressApplication } from '@nestjs/platform-express';
import cookieParser from 'cookie-parser';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);

  app.use(helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        frameAncestors: [`*`],
        objectSrc: [`'none'`],
        connectSrc: ["'self'"],
        baseUri: [`'self'`],
      },
    }
  }));

  app.useStaticAssets(join(__dirname, 'public'), {
    prefix: '/',
    extensions: ['html'],
  });

  app.setGlobalPrefix('api');

  app.use(cookieParser());
  app.useGlobalPipes(new ValidationPipe());

  app.useGlobalFilters(new NotFoundFilter());
  app.useGlobalFilters(new UnauthorizedFilter());

  const port = parseInt(process.env.PORT) || 3000;
  await app.listen(port);
}
bootstrap();
