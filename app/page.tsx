import HealthApp from './health-app';
import { getChatGPTUser } from './chatgpt-auth';
export const dynamic='force-dynamic';
export default async function Home(){const user=await getChatGPTUser();return <HealthApp signedIn={!!user} name={user?.displayName}/>}
