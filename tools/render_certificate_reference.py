import fitz

pdf = fitz.open(r"C:\Users\admin\Pictures\Screenshots\Certificate_Edusphere.pdf")
page = pdf[0]
pix = page.get_pixmap(matrix=fitz.Matrix(2, 2), alpha=False)
out = r"C:\Users\admin\Documents\Codex\2026-05-15\can-you-give-one-full-stack\tmp-certificate-reference.png"
pix.save(out)
print(out)
