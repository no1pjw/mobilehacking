export interface UserPayload {
    _id: string;
    username: string;
    name: string;
    role: 'user' | 'admin';
}

declare global {
    namespace Express {
        interface Request {
            user?: UserPayload;
        }
    }
}