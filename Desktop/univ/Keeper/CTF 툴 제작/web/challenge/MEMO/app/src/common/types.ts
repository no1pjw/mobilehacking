export type ResponseDto<T = void> = {
    status: number;
    message: string;
    data?: T;
}

export type TokenResponse = {
    accessToken: string;
    refreshToken?: string;
}