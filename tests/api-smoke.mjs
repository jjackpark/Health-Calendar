import assert from 'node:assert/strict';
const base='http://localhost:5173';
const login=await fetch(base+'/signin-with-chatgpt?return_to=/',{redirect:'manual'});
const cookie=login.headers.getSetCookie().map(x=>x.split(';')[0]).join('; ');
assert.ok(cookie,'local development login cookie');
const call=(path,method='GET',body,origin=base)=>fetch(base+path,{method,headers:{cookie,origin,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});
assert.equal((await fetch(base+'/api/preferences')).status,401);
assert.equal((await call('/api/records','PUT',{date:'2001-01-02',metric:'steps',value:1},'https://other.example')).status,403);
assert.equal((await call('/api/records','PUT',{date:'2001-02-30',metric:'steps',value:1})).status,400);
try{
 for(const value of [1234,5678])assert.equal((await call('/api/records','PUT',{date:'2001-01-02',metric:'steps',value})).status,200);
 assert.equal((await call('/api/records','PUT',{date:'2001-01-02',metric:'alcohol',value:0})).status,200);
 let data=await (await call('/api/records?from=2001-01-01&to=2001-01-03')).json();
 assert.equal(data.records.length,2);assert.equal(data.records.find(r=>r.metric==='steps').value,5678);
 assert.equal((await call('/api/records?date=2001-01-02&metric=steps','DELETE')).status,200);
 data=await (await call('/api/records?from=2001-01-01&to=2001-01-03')).json();assert.equal(data.records.length,1);assert.equal(data.records[0].metric,'alcohol');
 console.log('PASS: auth, cross-origin rejection, invalid dates, persistent upsert, selective deletion');
}finally{for(const metric of ['steps','alcohol'])await call('/api/records?date=2001-01-02&metric='+metric,'DELETE');}
