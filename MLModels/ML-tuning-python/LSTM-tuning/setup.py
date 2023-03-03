from setuptools import setup

setup(
    name='fil_lstm',
    version='0.1.0',    
    description='Run LSTM on 24h dataset',
    url='',
    author='Nghia Nguyen Danh',
    author_email='nghiadanh@gmail.com',
    license='FIL HUST',
    packages=['fil_lstm'],
    install_requires=['numpy==1.21.2',
                      'matplotlib==3.4.2',
                      'torch==1.11.0',
                      'torchvision==0.12.0',
                      'pandas==1.3.1',
                      'tqdm==4.62.2',          
                      ],

    classifiers=[
        'Programming Language :: Python :: 3.8.10',
    ],
)
