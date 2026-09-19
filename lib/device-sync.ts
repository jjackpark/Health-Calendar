import { z } from 'zod';
import { isDate } from './health';
export const pairSchema=z.object({code:z.string().transform(s=>s.replace(/[\s-]/g,'').toLowerCase()).pipe(z.string().regex(/^[a-f0-9]{24}$/)),name:z.string().trim().min(1).max(80)}).strict();
export const syncSchema=z.object({observedAt:z.number().int().positive(),timezone:z.string().max(80).refine(s=>{try{new Intl.DateTimeFormat('en',{timeZone:s});return true}catch{return false}}),background:z.boolean(),days:z.array(z.object({date:z.string().refine(isDate),steps:z.number().int().min(0).max(200000).nullable()}).strict()).min(1).max(31)}).strict().superRefine((s,c)=>{if(new Set(s.days.map(d=>d.date)).size!==s.days.length)c.addIssue({code:'custom',message:'중복 날짜는 전송할 수 없습니다.'})});
export const randomSecret=(bytes=32)=>Array.from(crypto.getRandomValues(new Uint8Array(bytes)),b=>b.toString(16).padStart(2,'0')).join('');
export async function hashSecret(value:string){return Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(value))),b=>b.toString(16).padStart(2,'0')).join('');}
export function windowAllowed(payload:z.infer<typeof syncSchema>,now=Date.now()){
 if(payload.observedAt>now+300000||payload.observedAt<now-86400000)return false;
 const parts=new Intl.DateTimeFormat('en-CA',{timeZone:payload.timezone,year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date(now));
 const get=(key:string)=>parts.find(p=>p.type===key)!.value;
 const today=`${get('year')}-${get('month')}-${get('day')}`;
 const earliest=new Date(new Date(today+'T00:00:00Z').getTime()-30*86400000).toISOString().slice(0,10);
 return payload.days.every(d=>d.date>=earliest&&d.date<=today);
}
