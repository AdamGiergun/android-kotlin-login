/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.firebaseui_login_sample

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.android.firebaseui_login_sample.databinding.FragmentMainBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth

class MainFragment : Fragment() {

    companion object {
        const val TAG = "MainFragment"
    }

    // Get a reference to the ViewModel scoped to this Fragment
    private val viewModel by viewModels<LoginViewModel>()
    private lateinit var binding: FragmentMainBinding

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)

        // TODO Remove the two lines below once observeAuthenticationState is implemented.
        binding.welcomeText.text = viewModel.getFactToDisplay(requireContext())
        binding.authButton.text = getString(R.string.login_btn)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAuthenticationState()

        binding.authButton.setOnClickListener {
            launchSignInFlow()
        }

        binding.settingsBtn.setOnClickListener {
            val action = MainFragmentDirections.actionMainFragmentToSettingsFragment()
            findNavController().navigate(action)
        }
    }

    private fun observeAuthenticationState() {
        val factToDisplay = viewModel.getFactToDisplay(requireContext())

        viewModel.authenticationState.observe(viewLifecycleOwner) { authenticationState ->
            when (authenticationState) {
                LoginViewModel.AuthenticationState.AUTHENTICATED -> {
                    binding.authButton.apply {
                        text = getString(R.string.logout_button_text)
                        setOnClickListener {
                            AuthUI.getInstance().signOut(requireContext())
                        }
                    }
                    binding.welcomeText.text = getFactWithPersonalization(factToDisplay)
                }
                else -> {
                    binding.authButton.apply {
                        text = getString(R.string.login_button_text)
                        setOnClickListener {
                            launchSignInFlow()
                        }
                    }
                    binding.welcomeText.text = factToDisplay
                }
            }
        }
    }


    private fun getFactWithPersonalization(fact: String): String {
        return String.format(
            resources.getString(
                R.string.welcome_message_authed,
                getDisplayName(),
                Character.toLowerCase(fact[0]) + fact.substring(1)
            )
        )
    }

    private fun getDisplayName() : String {
        return FirebaseAuth.getInstance().currentUser.let { user ->
            user?.displayName.let {
                var displayName = it
                if (displayName == null || displayName == "") {
                    val userInfoList = user?.providerData?.iterator()
                    userInfoList?.forEach uil@{ userInfo ->
                        displayName = userInfo.displayName
                        if (displayName != null) return@uil
                    }
                }
                if (displayName == null || displayName == "")
                    user?.email ?: getString(R.string.unknown_user)
                else
                    displayName
            } ?: getString(R.string.unknown_user)
        }
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            Log.i(
                TAG,
                "Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}"
            )
            //val user = FirebaseAuth.getInstance().currentUser
        } else {
            Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
        }
    }
}