import { createBrowserRouter } from 'react-router-dom';
import { ChatPage } from '../pages/ChatPage';
import { NotFoundPage } from '../pages/NotFoundPage';

export const router = createBrowserRouter([
  { path: '/', element: <ChatPage /> },
  { path: '*', element: <NotFoundPage /> },
]);
