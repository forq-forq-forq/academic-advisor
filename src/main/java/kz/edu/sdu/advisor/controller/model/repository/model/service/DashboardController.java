private final RegistrationAlertService alertService;

model.addAttribute("registrationAlert", alertService.getAlert(studentId).orElse(null));
