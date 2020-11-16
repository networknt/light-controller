import React, { useEffect, useState } from "react";
import CircularProgress from '@material-ui/core/CircularProgress';


export default function ServerInfo(props) {
    console.log(props);
    const node = props.location.state.data.node;

    const [info, setInfo] = useState();
    const [error, setError] = useState();
    const [loading, setLoading] = useState(true);
      
    const url = '/services/info/' + node;
    const headers = {};
    useEffect(() => {
        const abortController = new AbortController();
        const fetchData = async () => {
            setLoading(true);
            try {
                const response = await fetch(url, { headers, signal: abortController.signal });
                if (!response.ok) {
                    const data = await response.json();
                    console.log(data);
                    setError(data);
                    setLoading(false);
                } else {
                    const data = await response.json();
                    console.log(data);
                    setInfo(data);
                    setLoading(false);
                }
            } catch (e) {
                if (!abortController.signal.aborted) {
                    console.log(e);
                    setLoading(false);
                }
            }
        };

        fetchData();

        return () => {
            abortController.abort();
        };
    }, []);

    console.log(loading, info, error);
    let wait;
    if (loading) {
        wait = <div><CircularProgress /></div>;
    } else {
        wait = (
            <pre>{info ? JSON.stringify(info, null, 2) : error}</pre>
        )
    }
    return (
        <div>
            {wait}
        </div>
    );
}
