package ru.myitschool.normalplayer.ui.fragment.login.data;

import android.util.Log;

import java.io.IOException;

import retrofit2.Response;
import ru.myitschool.normalplayer.api.vk.model.response.VkTokenResponse;
import ru.myitschool.normalplayer.api.vk.token.VkTokenService;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    public Result<String> login(String username, String password) {
        Log.d("ASC", "login: ");
        try {
            Response<VkTokenResponse> tokenResponse = VkTokenService.getInstance().getVkTokenApi().getToken(username, password).execute();
            if (tokenResponse.body() != null) {
                if (tokenResponse.body().getToken() != null) {
                    Log.d("ASC", "login: " + tokenResponse.body().getToken());
                    return new Result.Success<>(tokenResponse.body().getToken());
                } else {
                    throw new Exception("Error");
                }
            } else {
                throw new Exception("Token error");
            }
        } catch (Exception e) {
            Log.e("ASC", "login: ", e);
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}