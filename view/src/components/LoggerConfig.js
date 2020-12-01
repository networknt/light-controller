import React, { useEffect, useState } from "react";
import CircularProgress from '@material-ui/core/CircularProgress';
import { makeStyles } from '@material-ui/core/styles';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Paper from '@material-ui/core/Paper';
import Button from '@material-ui/core/Button';

const useStyles = makeStyles({
    table: {
        minWidth: 650,
    },
});

export default function LoggerConfig(props) {
    const classes = useStyles();
    console.log(props.location.state.data);
    const node = props.location.state.data.node;
    const [loggers, setLoggers] = useState([]);
    const [error, setError] = useState();
    const [loading, setLoading] = useState(true);

    const handleLogger = () => {
        console.log(node, loggers);
        props.history.push({ pathname: '/form/loggerConfig', state: { data: { ...node, loggers } } });
    }

    const url = '/services/logger' + '?protocol=' + node.protocol + '&address=' + node.address + '&port=' + node.port;
    const headers = { 'Authorization': 'Basic ' + localStorage.getItem('user') };
    console.log(url);
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
                    setLoggers(data);
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

    console.log(loading, loggers, error);

    let wait;
    if (loading) {
        wait = <div><CircularProgress /></div>;
    } else if (loggers) {
        wait = (
            <>
            <TableContainer component={Paper}>
                <Table className={classes.table} aria-label="simple table">
                    <TableHead>
                        <TableRow>
                            <TableCell>Name</TableCell>
                            <TableCell align="right">Level</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {loggers.map((logger) => (
                            <TableRow key={logger.name}>
                                <TableCell component="th" scope="row">
                                    {logger.name}
                                </TableCell>
                                <TableCell align="right">{logger.level}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
            <Button variant="contained" color="primary" onClick={e => handleLogger()}>Update Logger Level</Button>
            </>
        )

    } else {
        wait = (
            <pre>{error}</pre>
        )
    }

    return (
        <div>
            {wait}
        </div>
    );
}
