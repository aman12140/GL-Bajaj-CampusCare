import { useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';

/** Loads data from a GET endpoint. Returns { data, loading, error, reload }. */
export function useFetch(url, params, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tick, setTick] = useState(0);

  const reload = useCallback(() => setTick((t) => t + 1), []);

  useEffect(() => {
    if (!url) return undefined;
    let cancelled = false;
    setLoading(true);
    setError('');
    api.get(url, { params })
      .then((res) => { if (!cancelled) setData(res.data); })
      .catch((err) => { if (!cancelled) setError(errorMessage(err)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [url, tick, ...deps]);

  return { data, loading, error, reload };
}

export function useDebounced(value, delay = 350) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}
