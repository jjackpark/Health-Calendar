import assert from 'node:assert/strict';
const base='http://localhost:5173';
const login=await fetch(base+'/signin-with-chatgpt?return_to=/',{redirect:'manual'});
const cookie=login.headers.getSetCookie().map(x=>x.split(';')[0]).join('; ');
const owner=method=>fetch(base+'/api/device',{method,headers:{cookie,origin:base}});
const pair=code=>fetch(base+'/api/device/pair',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({code,name:'Race test'})});
try {
 const {code}=await (await owner('POST')).json();const responses=await Promise.all([pair(code),pair(code)]);
 assert.deepEqual(responses.map(r=>r.status).sort(),[200,401]);
 await owner('DELETE');
 for(let i=0;i<5;i++){
  const {code}=await (await owner('POST')).json();const [response]=await Promise.all([pair(code),owner('DELETE')]);
  assert.ok([200,401].includes(response.status));
  assert.equal((await (await owner('GET')).json()).device,null,'Disconnect wins against concurrent pairing');
 }
 console.log('PASS: concurrent one-time claims and disconnect/pair races');
}finally{await owner('DELETE')}
