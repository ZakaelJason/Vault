package com.app.vault.marketplace

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.app.vault.marketplace.databinding.FragmentProfileBinding
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProfileFragment : Fragment() {
    private var _b: FragmentProfileBinding? = null
    private val b get() = _b!!
    private lateinit var db: DatabaseHelper
    private lateinit var sm: SessionManager
    private var selectedAvatarUri: Uri? = null
    private var currentAvatarPath: String = ""

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
        db = DatabaseHelper(requireContext())
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
            sm.clear()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun loadUserData() {
        val user = db.getUser(sm.getUserId())
        user?.let {
            b.etUsername.setText(it.username)
            b.etEmail.setText(it.email)
            b.etDescription.setText(it.description)
            currentAvatarPath = it.avatarUri
            
            if (currentAvatarPath.isNotEmpty()) {
                val file = File(currentAvatarPath)
                if (file.exists()) {
                    b.ivAvatar.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                } else {
                    b.ivAvatar.setImageResource(R.drawable.placeholder_profile)
                }
            } else {
                b.ivAvatar.setImageResource(R.drawable.placeholder_profile)
            }
        }
    }

    private fun saveProfile() {
        val finalAvatarPath = if (selectedAvatarUri != null) {
            copyImageToInternal(selectedAvatarUri!!) ?: currentAvatarPath
        } else {
            currentAvatarPath
        }

        val res = db.updateUser(
            sm.getUserId(),
            b.etUsername.text.toString(),
            b.etEmail.text.toString(),
            b.etDescription.text.toString(),
            finalAvatarPath
        )
        
        if (res > 0) {
            Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyImageToInternal(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = "avatar_${UUID.randomUUID()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
