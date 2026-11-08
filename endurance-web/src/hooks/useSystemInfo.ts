import { useQuery } from '@tanstack/react-query';
import { getSystemInfo } from '../lib/api';

export function useSystemInfo() {
  return useQuery({
    queryKey: ['system-info'],
    queryFn: ({ signal }) => getSystemInfo(signal),
    staleTime: 30_000,
    retry: false,
  });
}
