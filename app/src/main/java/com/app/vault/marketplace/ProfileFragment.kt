package com.app.vault.marketplace

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.app.vault.marketplace.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide

class ProfileFragment : Fragment() {
    private var _b: FragmentProfileBinding? = null
    private val b get() = _b!!
    private lateinit var sm: SessionManager
    private val repo = FirebaseRepository()
    private var selectedAvatarUri: Uri? = null
    private var currentAvatarUrl: String = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedAvatarUri = result.data?.data
            b.ivAvatar.setImageURI(selectedAvatarUri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sm = SessionManager(requireContext())

        loadUserData()

        b.cardAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImage.launch(intent)
        }

        b.btnCallCS.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:676767")
            }
            startActivity(intent)
        }

        b.btnSave.setOnClickListener {
            saveProfile()
        }

        b.btnLogout.setOnClickListener {
            repo.logout()
            sm.clear()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun loadUserData() {
        val uid = repo.currentUser?.uid ?: return
        repo.getUserProfile(
            uid = uid,
            onSuccess = { profile ->
                if (_b != null) {
                    b.etUsername.setText(profile.username)
                    b.etEmail.setText(profile.email)
                    b.etEmail.isEnabled = false // email terikat ke Firebase Auth, tidak diubah di sini
                    b.etDescription.setText(profile.description)
                    currentAvatarUrl = profile.avatarUrl

                    if (currentAvatarUrl.isNotEmpty()) {
                        Glide.with(this).load(currentAvatarUrl)
                            .placeholder(R.drawable.placeholder_profile)
                            .into(b.ivAvatar)
                    } else {
                        b.ivAvatar.setImageResource(R.drawable.placeholder_profile)
                    }
                }
            },
            onError = {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun saveProfile() {
        val uid = repo.currentUser?.uid ?: return
        val username = b.etUsername.text.toString().trim()
        val description = b.etDescription.text.toString().trim()

        b.btnSave.isEnabled = false

        if (selectedAvatarUri != null) {
            repo.uploadImage(
                uri = selectedAvatarUri!!,
                folder = "avatars",
                onSuccess = { url -> persistProfile(uid, username, description, url) },
                onError = {
                    b.btnSave.isEnabled = true
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            persistProfile(uid, username, description, currentAvatarUrl)
        }
    }

    private fun persistProfile(uid: String, username: String, description: String, avatarUrl: String) {
        repo.updateUserProfile(
            uid = uid, username = username, description = description, avatarUrl = avatarUrl,
            onSuccess = {
                if (_b != null) {
                    b.btnSave.isEnabled = true
                    currentAvatarUrl = avatarUrl
                    sm.saveProfile(uid, username)
                    Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { msg ->
                if (_b != null) {
                    b.btnSave.isEnabled = true
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
