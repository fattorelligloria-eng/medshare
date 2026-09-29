#!/usr/bin/env python3
"""
Gera o QR code que leva ao download do app.

Uso:
    python3 gerar-qrcode.py https://github.com/SEU-USUARIO/medshare/releases/latest

Sai um medshare-qrcode.png para colar no slide, no cartaz ou no grupo.
"""
import sys

try:
    import qrcode
    from qrcode.image.styledpil import StyledPilImage
    from qrcode.image.styles.moduledrawers.pil import RoundedModuleDrawer
    from qrcode.image.styles.colormasks import SolidFillColorMask
except ImportError:
    sys.exit("Instale a biblioteca primeiro:  pip install qrcode[pil]")

if len(sys.argv) < 2:
    sys.exit(__doc__)

endereco = sys.argv[1]
saida = sys.argv[2] if len(sys.argv) > 2 else "medshare-qrcode.png"

# Correção de erro alta: o QR continua legível mesmo impresso pequeno,
# amassado ou fotografado de longe no fundo da sala.
codigo = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_H, box_size=14, border=3)
codigo.add_data(endereco)
codigo.make(fit=True)

VERDE = (11, 140, 110)      # #0B8C6E, o verde da marca
FUNDO = (251, 252, 250)     # #FBFCFA

imagem = codigo.make_image(
    image_factory=StyledPilImage,
    module_drawer=RoundedModuleDrawer(),
    color_mask=SolidFillColorMask(back_color=FUNDO, front_color=VERDE),
)
imagem.save(saida)
print(f"QR code salvo em {saida}  ->  {endereco}")
