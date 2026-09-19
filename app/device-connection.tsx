'use client';
import {useEffect,useState} from 'react';
import {Copy,Download,Link2,RefreshCw,ShieldCheck} from 'lucide-react';
import {AlertDialog,AlertDialogContent,AlertDialogTitle,AlertDialogDescription,AlertDialogFooter,AlertDialogCancel,AlertDialogAction} from '@/components/ui/alert-dialog';
import {toast} from 'sonner';
export type Device={name:string;created_at:string;last_sync_at:string|null;background:number;timezone:string};
export function DeviceConnection({device,signedIn,onChanged}:{device:Device|null;signedIn:boolean;onChanged:()=>void}){
 const [code,setCode]=useState('');const [expires,setExpires]=useState(0);const [now,setNow]=useState(Date.now());const [busy,setBusy]=useState(false);const [error,setError]=useState('');const [confirm,setConfirm]=useState(false);
 useEffect(()=>{const timer=setInterval(()=>setNow(Date.now()),1000);return()=>clearInterval(timer)},[]);
 async function change(method:'POST'|'DELETE'){setBusy(true);setError('');try{const response=await fetch('/api/device',{method});const data=await response.json() as {code:string;expiresAt:number;error?:string};if(!response.ok)throw new Error(data.error??'연결 설정을 바꾸지 못했어요. 다시 시도해주세요.');if(method==='POST'){setCode(data.code);setExpires(data.expiresAt);setNow(Date.now())}else{setCode('');setConfirm(false);toast.success('휴대폰 연결을 해제했어요.')}onChanged();}catch(e){setError((e as Error).message)}finally{setBusy(false)}}
 const remaining=Math.max(0,Math.ceil((expires-now)/1000));
 return <div className="connection-info">
  <div className="connection-status"><ShieldCheck size={22}/><div><strong>{device?device.name:'연결할 휴대폰을 준비해주세요'}</strong><p>{device?.last_sync_at?'마지막 동기화 '+new Date(device.last_sync_at).toLocaleString('ko-KR'):device?'휴대폰 연결됨 · 첫 동기화 대기':'삼성헬스의 걸음 수를 가져옵니다.'}</p></div></div>
  {device&&<p className="muted">{device.background?'백그라운드 읽기 허용됨':'연동 앱에서 백그라운드 권한 상태를 확인해주세요.'}</p>}
  <section><h3>1. 연동 앱 설치</h3><p>삼성헬스가 있는 안드로이드 휴대폰에 설치해주세요.</p><a className="secondary" href="/downloads/health-calendar-sync.apk" download><Download size={18}/> 연동 앱 설치 파일 받기</a></section>
  <section><h3>2. 내 계정과 연결</h3><p>아래 코드를 복사해서 연동 앱에 붙여넣으세요. 코드는 10분 동안 한 번만 사용할 수 있어요.</p>{code&&remaining>0?<div className="pair-code"><code>{code.match(/.{1,4}/g)?.join('-')}</code><div className="row"><span>{Math.floor(remaining/60)}분 {remaining%60}초 남음</span><button onClick={async()=>{try{await navigator.clipboard.writeText(code);toast.success('연결 코드를 복사했어요.')}catch{toast.error('코드를 길게 눌러 직접 복사해주세요.')}}}><Copy size={16}/> 복사</button></div></div>:code?<p className="form-error">코드가 만료됐어요. 다시 발급해주세요.</p>:null}<button className="primary" disabled={busy||!signedIn} onClick={()=>change('POST')}><Link2 size={18}/>{code?'새 연결 코드 발급':'연결 코드 발급'}</button>{!signedIn&&<a href="/signin-with-chatgpt?return_to=/" target="_top">먼저 로그인해주세요.</a>}</section>
  <section><h3>3. 삼성헬스에서 공유 허용</h3><p>삼성헬스 → 설정 → 헬스 커넥트에서 <strong>걸음 쓰기</strong>를 허용해주세요. 연동 앱에서는 <strong>걸음 읽기</strong>와 지원되는 경우 <strong>백그라운드 읽기</strong>를 허용하면 됩니다.</p></section>
  <p className="footnote">최근 30일을 날짜별로 동기화합니다. 수동으로 입력한 걸음 수는 유지돼요. 자동 기록으로 바꾸려면 그 날짜의 수동 걸음 기록을 삭제하고 다시 동기화해주세요.</p>
  {error&&<p className="form-error" role="alert">{error}</p>}
  <button className="secondary" onClick={onChanged}><RefreshCw size={16}/> 연결 상태 새로고침</button>
  {device&&<button className="delete-button" onClick={()=>setConfirm(true)}>이 휴대폰 연결 해제</button>}
  <a href="/privacy" target="_blank" rel="noreferrer">건강 데이터 이용 안내 ↗</a>
  <AlertDialog open={confirm} onOpenChange={setConfirm}><AlertDialogContent><AlertDialogTitle>휴대폰 연결을 해제할까요?</AlertDialogTitle><AlertDialogDescription>새로운 걸음 수 전송을 중지합니다. 이미 저장한 기록은 유지돼요.</AlertDialogDescription><AlertDialogFooter><AlertDialogCancel disabled={busy}>취소</AlertDialogCancel><AlertDialogAction disabled={busy} onClick={e=>{e.preventDefault();void change('DELETE')}}>연결 해제</AlertDialogAction></AlertDialogFooter></AlertDialogContent></AlertDialog>
 </div>
}

