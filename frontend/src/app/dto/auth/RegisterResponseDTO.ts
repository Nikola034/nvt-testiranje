export interface RegisterResponseDto {
    id: number,
    message:string,
    username: string;
    name: string;
    surname: string;
    phoneNumber: string;
    accessToken: string,
    refreshToken: string
}