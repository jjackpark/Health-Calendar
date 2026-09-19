import type { Metadata, Viewport } from 'next';
import './globals.css';
export const metadata: Metadata = {title:'헬스 캘린더',description:'걸음, 운동, 수면, 음주를 나의 속도로 기록하세요.',icons:{icon:'/favicon.svg'},manifest:'/manifest.webmanifest',appleWebApp:{capable:true,statusBarStyle:'default',title:'헬스 캘린더'}};
export const viewport: Viewport = {width:'device-width',initialScale:1,viewportFit:'cover',themeColor:'#155b48'};
export default function Layout({children}:{children:React.ReactNode}){return <html lang="ko"><body>{children}</body></html>}
