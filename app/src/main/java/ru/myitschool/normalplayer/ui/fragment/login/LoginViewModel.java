package ru.myitschool.normalplayer.ui.fragment.login;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import ru.myitschool.normalplayer.R;
import ru.myitschool.normalplayer.ui.fragment.login.data.LoginRepository;
import ru.myitschool.normalplayer.ui.fragment.login.data.Result;

public class LoginViewModel extends ViewModel {

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private LoginRepository loginRepository;

    LoginViewModel(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    @SuppressLint("StaticFieldLeak")
    public void login(String username, String password) {
        // can be launched in a separate asynchronous job
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Result<String> result = loginRepository.login(username, password);

                if (result instanceof Result.Success) {
                    String data = ((Result.Success<String>) result).getData();
                    loginResult.postValue(new LoginResult(new LoggedInUserView(data)));
                } else {
                    loginResult.postValue(new LoginResult(R.string.login_failed));
                }
                return null;
            }
        }.execute();
    }

    //Just stub
    public void loginDataChanged(String username, String password) {
        loginFormState.setValue(new LoginFormState(true));
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return !username.trim().isEmpty();
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return true;//password != null && password.trim().length() > 5;
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {

        private final LoginRepository loginRepository;

        public Factory(LoginRepository loginRepository) {
            this.loginRepository = loginRepository;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new LoginViewModel(loginRepository);
        }
    }

}