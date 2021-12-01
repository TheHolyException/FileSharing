
document.querySelectorAll(".drop-zone__input").forEach((inputElement) => {
  const dropZoneElement = inputElement.closest(".drop-zone");

  dropZoneElement.addEventListener("click", (e) => {
    inputElement.click();
  });

  dropZoneElement.addEventListener("dragover", (e) => {
    e.preventDefault();
	document.getElementById('fileUpload').classList.add("btnUpload-drop");
  });

  ["dragleave", "dragend"].forEach((type) => {
    dropZoneElement.addEventListener(type, (e) => {
	document.getElementById('fileUpload').classList.remove("btnUpload-drop");
    });
  });

  dropZoneElement.addEventListener("drop", (e) => {
    e.preventDefault();

    if (e.dataTransfer.files.length) {
      inputElement.files = e.dataTransfer.files;
	  document.getElementById('form').submit();
    }
  });
});

