package com.springboot.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.springboot.dao.ContactRepository;
import com.springboot.dao.UserRepository;
import com.springboot.entities.Contact;
import com.springboot.entities.User;
import com.springboot.helper.Message;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String username = principal.getName();

		// getting the user using UserName(email)
		User user = userRepository.getUserByUserName(username);

		model.addAttribute(user);
	}

	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {

		model.addAttribute("title", "DASHBOARD");

		return "normal/user_dashboard";
	}

	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {

		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	/*
	 * @PostMapping("/process-contact") public String processContact(@ModelAttribute
	 * Contact contact) { System.out.println("Data:"+contact); return
	 * "normal/add_contact_form"; }
	 */

	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute("contact") @Valid Contact contact, BindingResult bindingResult,
			@RequestParam("profileImage") MultipartFile file, Model model, Principal principal, HttpSession session) {
		
		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);

			if (file.isEmpty()) {

				System.out.println("File is Empty");
				contact.setImage("contact.png");
			} else {
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Image is uploaded..");
			}
			contact.setUser(user);
			user.getContacts().add(contact);

			this.userRepository.save(user);

			session.setAttribute("message", new Message("Your Contact is added!! Add More..", "success"));
		} catch (Exception e) {

			System.out.println(e.getMessage());
			e.printStackTrace();

			session.setAttribute("message", new Message("Something went wrong..!! Try Again", "danger"));

		}
		return "normal/add_contact_form";
	}

	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "Show User Contacts");

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

//		CurrentPage-page
//		Contact Per Page - 5
		Pageable pageable = PageRequest.of(page, 1);
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);

		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("title", contact.getName());
			model.addAttribute("contact", contact);
		}
		return "normal/contact_detail";
	}

//	delete user

	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Model model, Principal principal,
			HttpSession session) {

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {

			user.getContacts().remove(contact);
			this.userRepository.save(user);

			session.setAttribute("message", new Message("Contact deleted successfully", "success"));
			return "redirect:/user/show-contacts/0";
		} else {
			
			model.addAttribute("title", "Access Denied!!");
			return "redirect:/user/show-contacts/0";
		}
	}
//	update update form handler

	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId") Integer cId, Model model) {

		model.addAttribute("title", "update Contact");

		Contact contact = this.contactRepository.findById(cId).get();

		model.addAttribute("contact", contact);
		
		return "normal/update_form";
	}

//update Contact Handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session, Principal principal) {
		try {
			//old contact details
			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
			
			if(!file.isEmpty()) {
				
				//delete old image
				File deleteFile = new ClassPathResource("static/image").getFile();
				File file1= new File(deleteFile, oldContactDetail.getImage());
				file1.delete();
				
				//update new Image
				File saveFile = new ClassPathResource("static/image").getFile();
				Path path = Paths.get(saveFile.getAbsoluteFile()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(),path , StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
				
			}else {
				contact.setImage(oldContactDetail.getImage());
				
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your Contact is Updated..", "success"));
			
		}catch (Exception e) {
            e.printStackTrace();
		}
		
	    return "redirect:/user/" + contact.getcId() + "/contact";
	}

//	profile handler	
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		
		model.addAttribute("title","Profile Page");
		
		return "normal/profile";
	}

}
