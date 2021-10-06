import React, { useState } from "react";
import Avatar from '@material-ui/core/Avatar';
import Button from '@material-ui/core/Button';
import CssBaseline from '@material-ui/core/CssBaseline';
import TextField from '@material-ui/core/TextField';
import LockOutlinedIcon from '@material-ui/icons/LockOutlined';
import Typography from '@material-ui/core/Typography';
import { makeStyles } from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';
import ErrorMessage from './ErrorMessage';
import { userService } from '../services/user';

const useStyles = makeStyles(theme => ({
    '@global': {
        body: {
            backgroundColor: theme.palette.common.white,
        },
    },
    paper: {
        marginTop: theme.spacing(8),
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
    },
    avatar: {
        margin: theme.spacing(1),
        backgroundColor: theme.palette.secondary.main,
    },
    form: {
        width: '100%', // Fix IE 11 issue.
        marginTop: theme.spacing(1),
    },
    submit: {
        margin: theme.spacing(3, 0, 2),
    },
    loginButtons: {
        width: '100%',
        marginTop: theme.spacing(1),
    }
}));

export default function Login(props) {
    const classes = useStyles();

    const [username, setUsername] = useState();
    const [password, setPassword] = useState();
    const [error, setError] = useState('');

    const handleChangeUsername = e => {
        setUsername(e.target.value)
    };

    const handleChangePassword = e => {
        setPassword(e.target.value)
    };

    const handleSubmit = event => {
        event.preventDefault();
        userService.login(username, password);
        const { from } = props.location.state || { from : { pathname: '/'} };
        props.history.push(from);
    };

    return (
        <Container component="main" maxWidth="xs">
            <CssBaseline />
            <div className={classes.paper}>
                <Avatar className={classes.avatar}>
                    <LockOutlinedIcon />
                </Avatar>
                <Typography component="h1" variant="h5">
                    Sign in
                </Typography>
                <ErrorMessage error={error} />
                <form className={classes.form} noValidate onSubmit={handleSubmit}>
                  <TextField
                        variant="outlined"
                        margin="normal"
                        required
                        fullWidth
                        id="username"
                        label="Username"
                        name="username"
                        value={username}
                        autoComplete="username"
                        autoFocus
                        onChange={handleChangeUsername}
                  />
                  <TextField
                        variant="outlined"
                        margin="normal"
                        required
                        fullWidth
                        name="password"
                        value={password}
                        label="Password"
                        type="password"
                        id="password"
                        autoComplete="password"
                        onChange={handleChangePassword}
                  />
                  <Button
                      type="submit"
                      fullWidth
                      variant="contained"
                      color="primary"
                      className={classes.submit}
                  >Sign In
                  </Button>
                </form>
            </div>
        </Container>
    );

}
